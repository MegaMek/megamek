/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

/** Per-instance rigid attachment. The asset library, never this binding, owns the shared mesh resources. */
final class UnitModelAttachment {
    private final ModelInstance parent;
    private final Node node;
    private final ModelInstance child;
    private final Matrix4 local;

    UnitModelAttachment(ModelInstance parent, ModelInstance child, UnitModelDescriptor.Hardpoint hardpoint,
          Matrix4 fit) {
        this.parent = parent;
        this.node = parent.getNode(hardpoint.node());
        if (node == null) {
            throw new IllegalArgumentException("Missing attachment parent: " + hardpoint.node());
        }
        this.child = child;
        this.local = hardpoint.transform().mul(fit);
    }

    /** Parent pose must have been evaluated first; this operation allocates no per-frame transforms. */
    void update() {
        child.transform.set(parent.transform).mul(node.globalTransform).mul(local);
    }

    /** The caller owns output vectors. No position or facing is inferred from game rules here. */
    static void emitter(ModelInstance instance, UnitModelDescriptor.Emitter emitter, Vector3 position,
          Vector3 direction) {
        Node node = instance.getNode(emitter.node());
        if (node == null) {
            throw new IllegalArgumentException("Missing emitter node: " + emitter.node());
        }
        emitter(instance, node, emitter, position, direction);
    }

    /** Formation members may share node IDs; resolve within the member before evaluating its world transform. */
    static void emitter(ModelInstance instance, Node node, UnitModelDescriptor.Emitter emitter, Vector3 position,
          Vector3 direction) {
        var point = emitter.position();
        var forward = emitter.direction();
        position.set(point.get(0), point.get(1), point.get(2)).mul(node.globalTransform).mul(instance.transform);
        direction.set(forward.get(0), forward.get(1), forward.get(2)).rot(node.globalTransform).rot(instance.transform).nor();
    }

    /** Authored barrel direction in the attachment parent's space, including its fitting and emitter transforms. */
    static Vector3 barrelDirection(ModelInstance instance, Node attachment, UnitModelDescriptor.Emitter emitter,
          Vector3 result) {
        Node source = emitter == null ? attachment : instance.getNode(emitter.node());
        if (emitter == null || source == null) {
            return result.set(Vector3.Y).mul(attachment.rotation).nor();
        }
        var forward = emitter.direction();
        result.set(forward.get(0), forward.get(1), forward.get(2)).rot(source.globalTransform);
        if (attachment.getParent() != null) {
            result.rot(attachment.getParent().globalTransform.cpy().inv());
        }
        return result.nor();
    }
}
