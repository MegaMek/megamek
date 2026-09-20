/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.IdentityHashMap;
import java.util.Map;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;

/** Picks the rendered rigid parts, including their current pose. CPU mesh copies live only with the view. */
final class UnitPicking {
    private record Geometry(float[] vertices, short[] indices, int stride, int position) { }

    private final Map<Mesh, Geometry> meshes = new IdentityHashMap<>();

    float distance(ModelInstance instance, Ray ray) {
        BoundingBox bounds = UnitBounds.world(instance);
        if (!Intersector.intersectRayBoundsFast(ray, bounds)) {
            return Float.POSITIVE_INFINITY;
        }
        float nearest = Float.POSITIVE_INFINITY;
        for (Node node : instance.nodes) {
            nearest = Math.min(nearest, distance(node, instance.transform, ray));
        }
        return nearest;
    }

    private float distance(Node node, Matrix4 root, Ray ray) {
        Matrix4 world = new Matrix4(root).mul(node.globalTransform);
        if (Math.abs(world.det()) < .0000001f) {
            return Float.POSITIVE_INFINITY;
        }
        Ray local = new Ray(ray.origin, ray.direction).mul(new Matrix4(world).inv());
        Vector3 a = new Vector3(), b = new Vector3(), c = new Vector3(), hit = new Vector3();
        float nearest = Float.POSITIVE_INFINITY;
        for (var part : node.parts) {
            if (!part.enabled) {
                continue;
            }
            Geometry geometry = meshes.computeIfAbsent(part.meshPart.mesh, UnitPicking::read);
            int end = part.meshPart.offset + part.meshPart.size;
            for (int index = part.meshPart.offset; index + 2 < end; index += 3) {
                vertex(geometry, index, a);
                vertex(geometry, index + 1, b);
                vertex(geometry, index + 2, c);
                if (Intersector.intersectRayTriangle(local, a, b, c, hit)) {
                    nearest = Math.min(nearest, ray.origin.dst2(hit.mul(world)));
                }
            }
        }
        for (Node child : node.getChildren()) {
            nearest = Math.min(nearest, distance(child, root, ray));
        }
        return nearest;
    }

    private static Geometry read(Mesh mesh) {
        int stride = mesh.getVertexSize() / Float.BYTES;
        float[] vertices = new float[mesh.getNumVertices() * stride];
        short[] indices = new short[mesh.getNumIndices()];
        mesh.getVertices(vertices);
        mesh.getIndices(indices);
        return new Geometry(vertices, indices, stride, mesh.getVertexAttribute(VertexAttributes.Usage.Position).offset / Float.BYTES);
    }

    private static void vertex(Geometry geometry, int index, Vector3 point) {
        int at = (geometry.indices().length == 0 ? index : Short.toUnsignedInt(geometry.indices()[index]))
              * geometry.stride() + geometry.position();
        point.set(geometry.vertices()[at], geometry.vertices()[at + 1], geometry.vertices()[at + 2]);
    }

    void clear() {
        meshes.clear();
    }
}
