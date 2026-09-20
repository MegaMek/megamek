/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

/** Shared posed bounds from the imported part bounds, without walking every vertex each frame. */
final class UnitBounds {
    private UnitBounds() { }

    static BoundingBox local(ModelInstance instance) {
        BoundingBox result = new BoundingBox().inf();
        instance.nodes.forEach(node -> extend(result, node));
        if (!result.isValid()) {
            result.set(Vector3.Zero, Vector3.Zero);
        }
        return result;
    }

    static BoundingBox world(ModelInstance instance) {
        return local(instance).mul(instance.transform);
    }

    static BoundingBox subtree(Node node) {
        var bounds = new BoundingBox().inf();
        extend(bounds, node);
        return bounds;
    }

    private static void extend(BoundingBox result, Node node) {
        for (var part : node.parts) {
            if (!part.enabled || part.meshPart.size == 0) {
                continue;
            }
            var mesh = part.meshPart;
            // G3DJ import supplies these bounds. Procedural/sprite fallback parts initialize them once on demand.
            if (mesh.radius < 0) {
                mesh.update();
            }
            result.ext(new BoundingBox(new Vector3(mesh.center).sub(mesh.halfExtents),
                  new Vector3(mesh.center).add(mesh.halfExtents)).mul(node.globalTransform));
        }
        node.getChildren().forEach(child -> extend(result, child));
    }
}
