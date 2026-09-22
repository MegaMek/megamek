/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

/** Shared posed bounds from the imported part bounds, without walking every vertex each frame. */
final class UnitBounds {
    private UnitBounds() { }

    static BoundingBox local(ModelInstance instance) {
        return local(instance, new BoundingBox(), new Vector3());
    }

    private static BoundingBox local(ModelInstance instance, BoundingBox result, Vector3 corner) {
        result.inf();
        for (var node : instance.nodes) { extend(result, node, corner); }
        if (!result.isValid()) {
            result.set(Vector3.Zero, Vector3.Zero);
        }
        return result;
    }

    static BoundingBox world(ModelInstance instance) {
        return local(instance).mul(instance.transform);
    }

    /** Render-thread snapshot. Begin after all poses/part visibility settle; returned boxes are borrowed until next begin. */
    static final class Frame {
        private final Map<ModelInstance, BoundingBox> bounds = new IdentityHashMap<>();
        private final List<BoundingBox> pool = new ArrayList<>();
        private final Vector3 corner = new Vector3();

        void begin() { bounds.clear(); }

        BoundingBox get(ModelInstance instance) {
            BoundingBox result = bounds.get(instance);
            if (result == null) {
                int index = bounds.size();
                if (index == pool.size()) { pool.add(new BoundingBox()); }
                result = local(instance, pool.get(index), corner).mul(instance.transform);
                bounds.put(instance, result);
            }
            return result;
        }
    }

    static BoundingBox subtree(Node node) {
        var bounds = new BoundingBox().inf();
        extend(bounds, node, new Vector3());
        return bounds;
    }

    private static void extend(BoundingBox result, Node node, Vector3 corner) {
        for (var part : node.parts) {
            if (!part.enabled || part.meshPart.size == 0) {
                continue;
            }
            var mesh = part.meshPart;
            // G3DJ import supplies these bounds. Procedural/sprite fallback parts initialize them once on demand.
            if (mesh.radius < 0) {
                mesh.update();
            }
            // An affine box transform is its transformed center plus the absolute basis times its half extents.
            // This produces the same eight-corner bounds with one point transform per rigid part.
            corner.set(mesh.center).mul(node.globalTransform);
            float[] matrix = node.globalTransform.val;
            float x = Math.abs(matrix[Matrix4.M00]) * mesh.halfExtents.x + Math.abs(matrix[Matrix4.M01]) * mesh.halfExtents.y
                  + Math.abs(matrix[Matrix4.M02]) * mesh.halfExtents.z;
            float y = Math.abs(matrix[Matrix4.M10]) * mesh.halfExtents.x + Math.abs(matrix[Matrix4.M11]) * mesh.halfExtents.y
                  + Math.abs(matrix[Matrix4.M12]) * mesh.halfExtents.z;
            float z = Math.abs(matrix[Matrix4.M20]) * mesh.halfExtents.x + Math.abs(matrix[Matrix4.M21]) * mesh.halfExtents.y
                  + Math.abs(matrix[Matrix4.M22]) * mesh.halfExtents.z;
            result.ext(corner.x - x, corner.y - y, corner.z - z);
            result.ext(corner.x + x, corner.y + y, corner.z + z);
        }
        for (var child : node.getChildren()) { extend(result, child, corner); }
    }
}
