/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.math.Vector3;
import megamek.common.board.Coords;

/** Render-owned cliff-top rim composition. Original artwork and game snapshots remain immutable. */
final class BoardRim {
    static final float BLEND_OPACITY = 0.7f;
    static final float GROUND_UV_SCALE = 0.96f; // MUST NOT TOUCH!!! With 1.0f we have some black pixels in the textures around the borders!

    record Images(BoardScene.Pixels color, BoardScene.Pixels normal) { }
    private record Triangle(float ax, float ay, float bx, float by, float cx, float cy) {
        boolean contains(float x, float y) {
            return cross(bx - ax, by - ay, cx - ax, cy - ay) > 0.0001f
                  && cross(bx - ax, by - ay, x - ax, y - ay) >= -0.0001f
                  && cross(cx - bx, cy - by, x - bx, y - by) >= -0.0001f
                  && cross(ax - cx, ay - cy, x - cx, y - cy) >= -0.0001f;
        }
    }
    private record Patch(int edge, float from, float to) { }
    private record Key(Images ground, BoardScene.Surface surface, List<Triangle> faces, List<Patch> patches) { }

    private final Map<Key, Images> cache = new HashMap<>();
    private final Set<Key> used = new HashSet<>();

    Images material(BoardScene scene, BoardScene.Tile tile, float floor, GpuAssets assets) {
        Images ground = new Images(tile.ground(), tile.normals());
        if (tile.liquid().present()) { return ground; }
        BoardSurface surface = new BoardSurface(scene, tile);
        List<BoardSurface.Side> sides = surface.sides(scene, floor);
        if (sides.isEmpty()) { return ground; }
        Vector3 center = BoardGeometry.center(tile.coords(), tile.elevation());
        List<Triangle> faces = new ArrayList<>();
        for (BoardSurface.Face face : surface.faces) {
            if (face.finish() == BoardSurface.Finish.TOP) {
                faces.add(new Triangle(local(face.a().x, center.x), local(face.a().y, center.y),
                      local(face.b().x, center.x), local(face.b().y, center.y),
                      local(face.c().x, center.x), local(face.c().y, center.y)));
            }
        }
        List<Patch> patches = new ArrayList<>();
        for (BoardSurface.Side side : sides) {
            Vector3 a = BoardGeometry.corner(tile.coords(), tile.elevation(), side.edge());
            Vector3 b = BoardGeometry.corner(tile.coords(), tile.elevation(), side.edge() + 1);
            Vector3 along = new Vector3(b).sub(a).nor();
            // Match the original clipped mesh: full edges extend into the corners; road mouths clip each end.
            float from = side.a().epsilonEquals(a, 0.02f) ? Float.NEGATIVE_INFINITY
                  : quantize(new Vector3(side.a()).sub(a).dot(along) / BoardGeometry.HEX_SCALE);
            float to = side.b().epsilonEquals(b, 0.02f) ? Float.POSITIVE_INFINITY
                  : quantize(new Vector3(side.b()).sub(a).dot(along) / BoardGeometry.HEX_SCALE);
            patches.add(new Patch(side.edge(), from, to));
        }
        Key key = new Key(ground, tile.surface(), List.copyOf(faces), List.copyOf(patches));
        used.add(key);
        return cache.computeIfAbsent(key, ignored -> compose(key, assets.inclineMask()));
    }

    /** End of one terrain snapshot update; keep only combinations used by that snapshot. */
    void retainUsed() {
        cache.keySet().retainAll(used);
        used.clear();
    }

    void clear() {
        cache.clear();
        used.clear();
    }

    private static float local(float value, float center) {
        return quantize((value - center) / BoardGeometry.HEX_SCALE);
    }

    private static float quantize(float value) {
        // A thousandth of an artwork pixel removes world-coordinate roundoff from otherwise identical keys.
        return Math.round(value * 1024) / 1024f;
    }

    private static float cross(float ax, float ay, float bx, float by) {
        return ax * by - ay * bx;
    }

    private static Images compose(Key key, Images rim) {
        BoardScene.Pixels ground = key.ground().color();
        int width = Math.max(ground.width(), (int) BoardGeometry.TILE_WIDTH);
        int height = Math.max(ground.height(), (int) BoardGeometry.TILE_HEIGHT);
        BufferedImage color = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        BufferedImage normal = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        float[] albedo = new float[4], detail = new float[4];
        Vector3 base = new Vector3(), combined = new Vector3(), bump = new Vector3();
        Vector3[] corners = new Vector3[6];
        Vector3[] along = new Vector3[6];
        var origin = new Coords(0, 0);
        Vector3 center = BoardGeometry.center(origin, 0);
        for (int edge = 0; edge < 6; edge++) {
            // Local artwork coordinates are independent of board scale and absolute tile position.
            corners[edge] = BoardGeometry.corner(origin, 0, edge).sub(center).scl(1 / BoardGeometry.HEX_SCALE);
        }
        for (int edge = 0; edge < 6; edge++) {
            along[edge] = new Vector3(corners[(edge + 1) % 6]).sub(corners[edge]);
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgba = texel(ground, x, y, width, height, albedo);
                float red = rgba >>> 24, green = rgba >>> 16 & 255, blue = rgba >>> 8 & 255;
                int packedNormal = key.ground().normal() == null ? 0x8080ffff
                      : texel(key.ground().normal(), x, y, width, height, detail);
                base.set(((packedNormal >>> 24) - 128) / 127f, ((packedNormal >>> 16 & 255) - 128) / 127f,
                      ((packedNormal >>> 8 & 255) - 128) / 127f).nor();
                float px = ((x + 0.5f) / width - 0.5f) * BoardGeometry.TILE_WIDTH / GROUND_UV_SCALE;
                float py = (0.5f - (y + 0.5f) / height) * BoardGeometry.TILE_HEIGHT / GROUND_UV_SCALE;
                boolean inside = false;
                for (Triangle face : key.faces()) {
                    if (face.contains(px, py)) { inside = true; break; }
                }
                boolean changed = false;
                if (inside) {
                    int coveredEdges = 0;
                    for (Patch patch : key.patches()) {
                        if ((coveredEdges & (1 << patch.edge())) != 0) { continue; }
                        Vector3 a = corners[patch.edge()], edge = along[patch.edge()];
                        float length = edge.len();
                        float tx = edge.x / length, ty = edge.y / length;
                        float distance = (px - a.x) * -ty + (py - a.y) * tx;
                        float position = (px - a.x) * tx + (py - a.y) * ty;
                        if (distance > 26 || position < patch.from() || position > patch.to()) { continue; }
                        coveredEdges |= 1 << patch.edge();
                        float u = 0.25f + position / (2 * length);
                        float v = 1 - distance / BoardGeometry.TILE_HEIGHT;
                        sample(rim.color(), u, v, albedo);
                        float alpha = albedo[3] / 255f * BLEND_OPACITY;
                        if (alpha <= 0) { continue; }
                        // The rim is a mask: gray is lightness about mid gray, so it leaves the top layer as it
                        // is at 128 and shades it darker or lighter where it lands, weighted by its own alpha.
                        float shade = 1 + alpha * (albedo[0] / 128f - 1);
                        red *= shade;
                        green *= shade;
                        blue *= shade;
                        if (rim.normal() != null) {
                            sample(rim.normal(), u, v, detail);
                            float nx = (detail[0] - 128) / 127f, ny = (detail[1] - 128) / 127f;
                            // Rim U follows the edge and V points outward; ground U is +X and V is -Y.
                            bump.set(tx * nx + ty * ny, -ty * nx + tx * ny, (detail[2] - 128) / 127f).nor();
                            blendNormal(base, bump, alpha, combined);
                            base.set(combined);
                            changed = true;
                        }
                    }
                }
                color.setRGB(x, y, ((rgba & 255) << 24) | (Math.round(red) << 16) | (Math.round(green) << 8) | Math.round(blue));
                normal.setRGB(x, y, changed ? 0xff000000 | (encode(base.x) << 16) | (encode(base.y) << 8) | encode(base.z)
                      : (packedNormal >>> 8) | 0xff000000);
            }
        }
        return new Images(new BoardScene.Pixels(color), new BoardScene.Pixels(normal));
    }

    /** Reoriented normal mapping: a neutral detail map preserves the base relief at any coverage. */
    static void blendNormal(Vector3 base, Vector3 detail, float alpha, Vector3 out) {
        float x = detail.x * alpha, y = detail.y * alpha, z = 1 + (detail.z - 1) * alpha;
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        x /= length;
        y /= length;
        z /= length;
        float dot = -base.x * x - base.y * y + (base.z + 1) * z;
        out.set(base.x, base.y, base.z + 1).scl(dot / Math.max(0.0001f, base.z + 1)).add(x, y, -z).nor();
    }

    private static int encode(float value) {
        return Math.clamp(Math.round(128 + 127 * value), 0, 255);
    }

    private static int texel(BoardScene.Pixels pixels, int x, int y, int width, int height, float[] scratch) {
        if (pixels.width() == width && pixels.height() == height) { return pixels.rgba(y * width + x); }
        sample(pixels, (x + 0.5f) / width, (y + 0.5f) / height, scratch);
        return Math.round(scratch[0]) << 24 | Math.round(scratch[1]) << 16 | Math.round(scratch[2]) << 8 | Math.round(scratch[3]);
    }

    /** Match GL linear sampling, including the texel-centre convention and clamp-to-edge addressing. */
    private static void sample(BoardScene.Pixels pixels, float u, float v, float[] out) {
        float x = Math.clamp(u * pixels.width() - 0.5f, 0, pixels.width() - 1);
        float y = Math.clamp(v * pixels.height() - 0.5f, 0, pixels.height() - 1);
        int x0 = (int) x, y0 = (int) y;
        int x1 = Math.min(x0 + 1, pixels.width() - 1), y1 = Math.min(y0 + 1, pixels.height() - 1);
        int a = pixels.rgba(y0 * pixels.width() + x0), b = pixels.rgba(y0 * pixels.width() + x1);
        int c = pixels.rgba(y1 * pixels.width() + x0), d = pixels.rgba(y1 * pixels.width() + x1);
        for (int channel = 0; channel < 4; channel++) {
            int shift = 24 - 8 * channel;
            float top = ((a >>> shift) & 255) + (((b >>> shift) & 255) - ((a >>> shift) & 255)) * (x - x0);
            float bottom = ((c >>> shift) & 255) + (((d >>> shift) & 255) - ((c >>> shift) & 255)) * (x - x0);
            out[channel] = top + (bottom - top) * (y - y0);
        }
    }
}
