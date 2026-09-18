/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

/**
 * The extruded volume of a unit token's alpha cutout. Terrain uses authored models.
 * Polygons carry the artwork across the top (and bottom) face and walls close the shape along its alpha
 * contour, so the artwork reads as a flat-topped solid instead of a sprite. Vertices are written in artwork
 * pixels, centered on the artwork, between {@code origin.z} (bottom) and {@code origin.z + height} (top); the
 * caller scales the hex plane and places the volume.
 */
final class GpuCutout {
    private static final int ALPHA_THRESHOLD = 128;

    private GpuCutout() { }

    /**
     * Extrudes the artwork's opaque pixels into {@code caps} and {@code sides}. The faces sample the artwork
     * through {@code region} and the walls carry the artwork's alpha-weighted average color in their vertex
     * color; {@code bottomCap} can be left out where the volume stands on a surface that hides it. The
     * volume's footprint is the artwork's own pixels scaled by {@code scale} around its center. Returns the
     * vertices written, so a caller that packs many cutouts into one mesh part can start the next part before
     * the mesh builder's vertex limit.
     */
    static int extrude(BoardScene.Pixels pixels, TextureRegion region, Vector3 origin, float scale, float height,
          boolean bottomCap, MeshPartBuilder caps, MeshPartBuilder sides) {
        Color side = averageColor(pixels);
        return smoothGeometry(pixels, region, origin, scale, height, bottomCap, side, caps, sides);
    }

    /**
     * Cut the faces along the sampled alpha values and close their edges with walls. Cells that are filled at
     * every corner merge into one quad per run, because their faces are plain rectangles of the artwork; only
     * cells whose corner carries the alpha edge need their own contour polygons.
     */
    private static int smoothGeometry(BoardScene.Pixels pixels, TextureRegion region, Vector3 origin, float scale,
          float height, boolean bottomCap, Color side, MeshPartBuilder caps, MeshPartBuilder sides) {
        int vertices = 0;
        for (int row = -1; row < pixels.height(); row++) {
            int runStart = -1;
            for (int column = -1; column < pixels.width(); column++) {
                Vector3[] corners = {
                      sample(pixels, column, row), sample(pixels, column + 1, row),
                      sample(pixels, column + 1, row + 1), sample(pixels, column, row + 1)
                };
                int filled = 0;
                for (Vector3 corner : corners) {
                    if (corner.z >= ALPHA_THRESHOLD) {
                        filled++;
                    }
                }
                if (filled == 4) {
                    if (runStart < 0) {
                        runStart = column;
                    }
                    continue;
                }
                if (runStart >= 0) {
                    vertices += capRun(caps, pixels, region, origin, scale, height, runStart, column, row,
                          bottomCap);
                    runStart = -1;
                }
                if (filled == 0) {
                    continue;
                }
                Vector3 center = new Vector3();
                for (Vector3 corner : corners) {
                    center.add(corner);
                }
                center.scl(0.25f);
                for (int edge = 0; edge < 4; edge++) {
                    List<Vector3> polygon = alphaContour(List.of(center, corners[edge], corners[(edge + 1) % 4]));
                    vertices += surface(pixels, region, origin, scale, height, polygon, bottomCap, caps);
                    for (int index = 0; index < polygon.size(); index++) {
                        Vector3 start = polygon.get(index);
                        Vector3 end = polygon.get((index + 1) % polygon.size());
                        if (start.z == ALPHA_THRESHOLD && end.z == ALPHA_THRESHOLD
                              && start.dst2(end) > 0.000001f) {
                            Vector3 normal = new Vector3(end.y - start.y, end.x - start.x, 0).nor();
                            sides.rect(vertex(pixels, region, origin, scale, height, start.x, start.y, 0, 1)
                                          .setNor(normal).setCol(side),
                                  vertex(pixels, region, origin, scale, height, end.x, end.y, 0, 1)
                                        .setNor(normal).setCol(side),
                                  vertex(pixels, region, origin, scale, height, end.x, end.y, 1, 1)
                                        .setNor(normal).setCol(side),
                                  vertex(pixels, region, origin, scale, height, start.x, start.y, 1, 1)
                                        .setNor(normal).setCol(side));
                            vertices += 4;
                        }
                    }
                }
            }
        }
        return vertices;
    }

    /** One run of fully filled cells as a single quad per face, spanning the columns before {@code end}. */
    private static int capRun(MeshPartBuilder caps, BoardScene.Pixels pixels, TextureRegion region, Vector3 origin,
          float scale, float height, int start, int end, int row, boolean bottomCap) {
        int vertices = 0;
        if (bottomCap) {
            caps.rect(vertex(pixels, region, origin, scale, height, start, row, 0, 0),
                  vertex(pixels, region, origin, scale, height, end, row, 0, 0),
                  vertex(pixels, region, origin, scale, height, end, row + 1, 0, 0),
                  vertex(pixels, region, origin, scale, height, start, row + 1, 0, 0));
            vertices += 4;
        }
        caps.rect(vertex(pixels, region, origin, scale, height, start, row, 1, 1),
              vertex(pixels, region, origin, scale, height, end, row, 1, 1),
              vertex(pixels, region, origin, scale, height, end, row + 1, 1, 1),
              vertex(pixels, region, origin, scale, height, start, row + 1, 1, 1));
        return vertices + 4;
    }

    /** One sample per pixel corner: its position in artwork pixels and its alpha, from which contours are cut. */
    private static Vector3 sample(BoardScene.Pixels pixels, int column, int row) {
        int alpha = column < 0 || row < 0 || column >= pixels.width() || row >= pixels.height()
              ? 0 : pixels.rgba(row * pixels.width() + column) & 0xff;
        return new Vector3(column + 0.5f, row + 0.5f, alpha);
    }

    static List<Vector3> alphaContour(List<Vector3> triangle) {
        List<Vector3> polygon = new ArrayList<>();
        Vector3 previous = triangle.getLast();
        for (Vector3 current : triangle) {
            if ((previous.z >= ALPHA_THRESHOLD) != (current.z >= ALPHA_THRESHOLD)) {
                Vector3 crossing = new Vector3(previous).lerp(current,
                      (ALPHA_THRESHOLD - previous.z) / (current.z - previous.z));
                crossing.z = ALPHA_THRESHOLD;
                polygon.add(crossing);
            }
            if (current.z >= ALPHA_THRESHOLD) {
                polygon.add(current);
            }
            previous = current;
        }
        return polygon;
    }

    /** Both faces of one contour polygon; the bottom face is left out where the caller has no use for it. */
    private static int surface(BoardScene.Pixels pixels, TextureRegion region, Vector3 origin, float scale,
          float height, List<Vector3> polygon, boolean bottomCap, MeshPartBuilder caps) {
        int vertices = 0;
        for (int index = 1; index + 1 < polygon.size(); index++) {
            Vector3 first = polygon.getFirst();
            Vector3 second = polygon.get(index);
            Vector3 third = polygon.get(index + 1);
            if (bottomCap) {
                caps.triangle(vertex(pixels, region, origin, scale, height, first.x, first.y, 0, 0),
                      vertex(pixels, region, origin, scale, height, second.x, second.y, 0, 0),
                      vertex(pixels, region, origin, scale, height, third.x, third.y, 0, 0));
                vertices += 3;
            }
            caps.triangle(vertex(pixels, region, origin, scale, height, first.x, first.y, 1, 1),
                  vertex(pixels, region, origin, scale, height, second.x, second.y, 1, 1),
                  vertex(pixels, region, origin, scale, height, third.x, third.y, 1, 1));
            vertices += 3;
        }
        return vertices;
    }

    static Color averageColor(BoardScene.Pixels pixels) {
        double red = 0;
        double green = 0;
        double blue = 0;
        long weight = 0;
        for (int index = 0; index < pixels.width() * pixels.height(); index++) {
            int rgba = pixels.rgba(index);
            int alpha = rgba & 0xff;
            red += (rgba >>> 24) * alpha;
            green += ((rgba >>> 16) & 0xff) * alpha;
            blue += ((rgba >>> 8) & 0xff) * alpha;
            weight += alpha;
        }
        return weight == 0 ? new Color(Color.BLACK)
              : new Color((float) (red / weight / 255), (float) (green / weight / 255),
                    (float) (blue / weight / 255), 1);
    }

    /** One cutout vertex in artwork pixels: {@code t} runs from the volume's bottom (0) to its top (1). */
    private static MeshPartBuilder.VertexInfo vertex(BoardScene.Pixels pixels, TextureRegion region, Vector3 origin,
          float scale, float height, float column, float row, float t, float shade) {
        return new MeshPartBuilder.VertexInfo()
              .setPos(origin.x + (column - pixels.width() / 2f) * scale,
                    origin.y + (pixels.height() / 2f - row) * scale, origin.z + t * height)
              .setNor(0, 0, t > 0 ? 1 : -1)
              .setCol(shade, shade, shade, 1)
              .setUV(MathUtils.lerp(region.getU(), region.getU2(), column / pixels.width()),
                    MathUtils.lerp(region.getV(), region.getV2(), row / pixels.height()));
    }
}
