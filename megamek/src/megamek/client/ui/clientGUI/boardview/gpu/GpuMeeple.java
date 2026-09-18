/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Disposable;

final class GpuMeeple implements Disposable {
    private static final int ALPHA_THRESHOLD = 128;
    private static final boolean ANTIALIASING = true;
    private final Model model;
    final ModelInstance instance;
    private final BoundingBox bounds;

    GpuMeeple(BoardScene.Pixels pixels, TextureRegion region) {
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        MeshPartBuilder mesh = builder.part("cutout", GL20.GL_TRIANGLES,
              VertexAttributes.Usage.Position | VertexAttributes.Usage.TextureCoordinates | VertexAttributes.Usage.ColorPacked
                  | VertexAttributes.Usage.Normal,
              new Material(TextureAttribute.createDiffuse(region.getTexture()), IntAttribute.createCullFace(GL20.GL_NONE)));
        MeshPartBuilder sides = builder.part("sides", GL20.GL_TRIANGLES,
              VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
              new Material(ColorAttribute.createDiffuse(averageColor(pixels)), IntAttribute.createCullFace(GL20.GL_NONE)));
        if (ANTIALIASING) {
            smoothGeometry(pixels, region, mesh, sides);
        } else {
        for (int row = 0; row < pixels.height(); row++) {
            int runStart = -1;
            for (int column = 0; column <= pixels.width(); column++) {
                boolean filled = opaque(pixels, column, row);
                if (filled) {
                    if (runStart < 0) {
                        runStart = column;
                    }
                    for (int edge = 0; edge < 4; edge++) {
                        int neighborX = column + (edge == 1 ? 1 : edge == 3 ? -1 : 0);
                        int neighborY = row + (edge == 0 ? -1 : edge == 2 ? 1 : 0);
                        if (!opaque(pixels, neighborX, neighborY)) {
                            float left = column + (edge == 1 || edge == 2 ? 1 : 0);
                            float top = row + (edge >= 2 ? 1 : 0);
                            float right = column + (edge <= 1 ? 1 : 0);
                            float bottom = row + (edge == 1 || edge == 2 ? 1 : 0);
                                        Vector3 normal = new Vector3(neighborX - column, row - neighborY, 0);
                                        sides.rect(vertex(pixels, region, left, top, -0.5f, 1).setNor(normal),
                                            vertex(pixels, region, right, bottom, -0.5f, 1).setNor(normal),
                                            vertex(pixels, region, right, bottom, 0.5f, 1).setNor(normal),
                                            vertex(pixels, region, left, top, 0.5f, 1).setNor(normal));
                        }
                    }
                } else if (runStart >= 0) {
                    for (float depth : new float[] { -0.5f, 0.5f }) {
                        float shade = depth > 0 ? 1 : 0;
                        mesh.rect(vertex(pixels, region, runStart, row, depth, shade),
                              vertex(pixels, region, runStart, row + 1, depth, shade),
                              vertex(pixels, region, column, row + 1, depth, shade),
                              vertex(pixels, region, column, row, depth, shade));
                    }
                    runStart = -1;
                }
            }
        }
        }
        model = builder.end();
        instance = new ModelInstance(model);
        bounds = instance.calculateBoundingBox(new BoundingBox());
        if (!bounds.isValid()) {
            bounds.set(Vector3.Zero, Vector3.Zero);
        }
    }

    private static void smoothGeometry(BoardScene.Pixels pixels, TextureRegion region,
          MeshPartBuilder mesh, MeshPartBuilder sides) {
        for (int row = -1; row < pixels.height(); row++) {
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
                if (filled == 0) {
                    continue;
                }
                if (filled == 4) {
                    surface(pixels, region, mesh, List.of(corners));
                    continue;
                }
                Vector3 center = new Vector3();
                for (Vector3 corner : corners) {
                    center.add(corner);
                }
                center.scl(0.25f);
                for (int edge = 0; edge < 4; edge++) {
                    List<Vector3> polygon = alphaContour(List.of(center, corners[edge], corners[(edge + 1) % 4]));
                    surface(pixels, region, mesh, polygon);
                    for (int index = 0; index < polygon.size(); index++) {
                        Vector3 start = polygon.get(index);
                        Vector3 end = polygon.get((index + 1) % polygon.size());
                        if (start.z == ALPHA_THRESHOLD && end.z == ALPHA_THRESHOLD
                              && start.dst2(end) > 0.000001f) {
                            Vector3 normal = new Vector3(end.y - start.y, end.x - start.x, 0).nor();
                            sides.rect(vertex(pixels, region, start.x, start.y, -0.5f, 1).setNor(normal),
                                  vertex(pixels, region, end.x, end.y, -0.5f, 1).setNor(normal),
                                  vertex(pixels, region, end.x, end.y, 0.5f, 1).setNor(normal),
                                  vertex(pixels, region, start.x, start.y, 0.5f, 1).setNor(normal));
                        }
                    }
                }
            }
        }
    }

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

    private static void surface(BoardScene.Pixels pixels, TextureRegion region,
          MeshPartBuilder mesh, List<Vector3> polygon) {
        for (int index = 1; index + 1 < polygon.size(); index++) {
            Vector3 first = polygon.getFirst();
            Vector3 second = polygon.get(index);
            Vector3 third = polygon.get(index + 1);
            for (float depth : new float[] { -0.5f, 0.5f }) {
                float shade = depth > 0 ? 1 : 0;
                mesh.triangle(vertex(pixels, region, first.x, first.y, depth, shade),
                      vertex(pixels, region, second.x, second.y, depth, shade),
                      vertex(pixels, region, third.x, third.y, depth, shade));
            }
        }
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

    private static boolean opaque(BoardScene.Pixels pixels, int column, int row) {
        return column >= 0 && row >= 0 && column < pixels.width() && row < pixels.height()
              && (pixels.rgba(row * pixels.width() + column) & 0xff) >= ALPHA_THRESHOLD;
    }

    private static MeshPartBuilder.VertexInfo vertex(BoardScene.Pixels pixels, TextureRegion region,
          float column, float row, float depth, float shade) {
        return new MeshPartBuilder.VertexInfo().setPos(column - pixels.width() / 2f, pixels.height() / 2f - row, depth)
              .setNor(0, 0, depth > 0 ? 1 : -1)
              .setCol(shade, shade, shade, 1)
              .setUV(MathUtils.lerp(region.getU(), region.getU2(), column / pixels.width()),
                    MathUtils.lerp(region.getV(), region.getV2(), row / pixels.height()));
    }

    Vector3 place(ModelInstance placed, Camera camera, Vector3 ground, float facing, int height) {
        float thickness = height * BoardGeometry.LEVEL * BoardGeometry.UNIT_HEIGHT_SCALE;
        placed.transform.set(ground, new Quaternion(Vector3.Z, -facing))
              .translate(0, 0, thickness / 2 + 0.5f)
              .scale(BoardGeometry.UNIT_SCALE, BoardGeometry.UNIT_SCALE, thickness);
        float top = -Float.MAX_VALUE;
        for (float horizontal : new float[] { bounds.min.x, bounds.max.x }) {
            for (float vertical : new float[] { bounds.min.y, bounds.max.y }) {
                for (float depth : new float[] { bounds.min.z, bounds.max.z }) {
                    top = Math.max(top, new Vector3(horizontal, vertical, depth).rot(placed.transform).dot(camera.up));
                }
            }
        }
        return placed.transform.getTranslation(new Vector3()).mulAdd(camera.up, top);
    }

    @Override
    public void dispose() {
        model.dispose();
    }
}
