/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

/** Tiny untextured details, baked into a single opaque mesh per terrain chunk. */
final class GpuScatter {
    private static final int[][] ROCK_FACES = {
          { 0, 1, 5 }, { 1, 2, 6 }, { 1, 6, 5 }, { 2, 3, 6 }, { 3, 4, 7 },
          { 3, 7, 6 }, { 4, 0, 5 }, { 4, 5, 7 }, { 5, 6, 7 }
    };

    private GpuScatter() { }

    static float diameter(BoardScene.Feature feature) {
        return (float) Math.hypot(8 * feature.scale() * BoardGeometry.HEX_SCALE,
              feature.height() * BoardGeometry.LEVEL);
    }

    static void build(MeshPartBuilder mesh, BoardScene.Tile tile, BoardSurface surface, BoardScene.Feature feature) {
        float x = BoardGeometry.centerX(tile.coords()) + feature.x() * BoardGeometry.HEX_SCALE;
        float y = BoardGeometry.centerY(tile.coords()) + feature.y() * BoardGeometry.HEX_SCALE;
        Matrix4 transform = new Matrix4().setToTranslation(x, y, surface.height(x, y))
              .rotate(Vector3.Z, feature.rotation())
              .scale(feature.scale() * BoardGeometry.HEX_SCALE, feature.scale() * BoardGeometry.HEX_SCALE,
                    feature.height() * BoardGeometry.LEVEL);
        Color color = color(tile.surface(), feature.asset());
        float shade = .88f + .24f * feature.rotation() / 360;
        color.mul(shade, shade, shade, 1);
        switch (feature.asset()) {
            case "scatter-grass", "scatter-dry-grass" -> {
                // Three splayed blades; back faces keep them visible from every board rotation.
                for (int blade = 0; blade < 3; blade++) {
                    Matrix4 direction = new Matrix4(transform).rotate(Vector3.Z, blade * 137.5f);
                    triangle(mesh, point(-.5f, 0, 0, direction), point(.5f, 0, 0, direction),
                          point(.8f, 2.5f - blade * .35f, 1 - blade * .15f, direction), color, true);
                }
            }
            case "scatter-plant" -> {
                // Four broad, creased leaves also read as a low succulent on sand.
                for (int leaf = 0; leaf < 4; leaf++) {
                    Matrix4 direction = new Matrix4(transform).rotate(Vector3.Z, leaf * 97);
                    Vector3 root = point(0, 0, .05f, direction);
                    Vector3 left = point(-.85f, 1.7f, .85f, direction);
                    Vector3 tip = point(.2f, 3.6f - leaf * .15f, .4f, direction);
                    Vector3 right = point(.8f, 1.5f, .55f, direction);
                    triangle(mesh, root, right, tip, color, true);
                    triangle(mesh, root, tip, left, color, true);
                }
            }
            case "scatter-rock", "scatter-slab" -> {
                float height = feature.asset().equals("scatter-slab") ? .4f : 1;
                Vector3[] points = {
                      point(-3, -1, -.08f, transform), point(-.8f, -2.4f, -.08f, transform),
                      point(2.9f, -1.8f, -.08f, transform), point(3, 1.3f, -.08f, transform),
                      point(-1.4f, 2.3f, -.08f, transform), point(-1.4f, -.8f, .75f * height, transform),
                      point(1.4f, -.5f, height, transform), point(.2f, 1.3f, .78f * height, transform)
                };
                for (int[] face : ROCK_FACES) {
                    triangle(mesh, points[face[0]], points[face[1]], points[face[2]], color, false);
                }
                // The open underside is buried in the ground, so it needs no triangles.
            }
            default -> throw new IllegalArgumentException("Unknown terrain scatter: " + feature.asset());
        }
    }

    private static Color color(BoardScene.Surface surface, String asset) {
        if (asset.equals("scatter-dry-grass")) {
            return new Color(.51f, .43f, .26f, 1);
        }
        if (asset.equals("scatter-grass") || asset.equals("scatter-plant")) {
            return surface == BoardScene.Surface.SAND ? new Color(.37f, .44f, .26f, 1)
                  : asset.equals("scatter-grass") ? new Color(.33f, .4f, .17f, 1) : new Color(.25f, .36f, .16f, 1);
        }
        return switch (surface) {
            case SAND -> new Color(.61f, .49f, .33f, 1);
            case DIRT -> new Color(.47f, .32f, .24f, 1);
            case SNOW -> new Color(.65f, .66f, .67f, 1);
            case ROCK -> new Color(.49f, .48f, .45f, 1);
            default -> new Color(.43f, .45f, .38f, 1);
        };
    }

    private static Vector3 point(float x, float y, float z, Matrix4 transform) {
        return new Vector3(x, y, z).mul(transform);
    }

    private static void triangle(MeshPartBuilder mesh, Vector3 a, Vector3 b, Vector3 c, Color color, boolean back) {
        Vector3 normal = new Vector3(b).sub(a).crs(new Vector3(c).sub(a)).nor();
        mesh.triangle(vertex(a, normal, color), vertex(b, normal, color), vertex(c, normal, color));
        if (back) {
            normal.scl(-1);
            mesh.triangle(vertex(c, normal, color), vertex(b, normal, color), vertex(a, normal, color));
        }
    }

    private static MeshPartBuilder.VertexInfo vertex(Vector3 point, Vector3 normal, Color color) {
        return new MeshPartBuilder.VertexInfo().setPos(point).setNor(normal).setCol(color).setUV(0, 0);
    }
}
