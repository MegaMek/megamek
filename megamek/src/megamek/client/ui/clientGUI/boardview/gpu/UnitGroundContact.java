/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import megamek.common.units.EntityMovementType;

/** Small rigid support-plane adjustment after world placement; the board's existing surfaces remain authoritative. */
final class UnitGroundContact {
    private record Contact(Node node, Vector3 point) { }
    private record Body(UnitRig rig, Node root, BoundingBox bounds, List<Contact> contacts) { }
    private final ModelInstance instance;
    private final BoardSurface.Cache surfaces;
    private final List<Body> bodies = new ArrayList<>();

    UnitGroundContact(ModelInstance instance, ModelInstance rest, List<UnitRig> rigs, BoardSurface.Cache surfaces) {
        this.instance = instance;
        this.surfaces = surfaces;
        for (var rig : rigs) {
            if (!(rig.mek() || rig.trooper() || rig.transport() || "vehicle-v1".equals(rig.type()) || "proto-v1".equals(rig.type()))) {
                continue;
            }
            var nodes = rig.container() == null ? instance.nodes : instance.getNode(rig.container()).getChildren();
            var originals = rig.container() == null ? rest.nodes : rest.getNode(rig.container()).getChildren();
            Node root = UnitAnimator.find(nodes, rig.joints().get("root"));
            var bounds = localBounds(UnitAnimator.find(originals, rig.joints().get("root")));
            List<Contact> contacts = new ArrayList<>();
            rig.joints().forEach((role, id) -> {
                if (role.endsWith("Foot")) {
                    var sole = localBounds(UnitAnimator.find(originals, id));
                    contacts.add(new Contact(UnitAnimator.find(nodes, id), new Vector3(sole.getCenterX(), sole.getCenterY(), sole.min.z)));
                }
            });
            if (contacts.isEmpty()) {
                for (float x : new float[] { bounds.min.x, bounds.max.x }) {
                    for (float y : new float[] { bounds.min.y, bounds.max.y }) {
                        contacts.add(new Contact(root, new Vector3(x, y, bounds.min.z)));
                    }
                }
            }
            bodies.add(new Body(rig, root, bounds, contacts));
        }
    }

    private static BoundingBox localBounds(Node original) {
        var copy = original.copy();
        copy.translation.setZero();
        copy.rotation.idt();
        copy.scale.set(1, 1, 1);
        copy.calculateTransforms(true);
        return UnitBounds.subtree(copy);
    }

    boolean apply(BoardScene scene, BoardScene.Unit unit, UnitMotion.Sample motion) {
        if (unit.sensorContact() || unit.footprint().size() > 1 || motion.airborne(unit)) { return false; }
        boolean changed = false;
        for (var body : bodies) {
            var member = motion.member(unit.id(), body.rig.container());
            if (member.type() == EntityMovementType.MOVE_JUMP || !UnitBounds.subtree(body.root).isValid()) { continue; }
            if (body.rig.mek() && (member.posture() != null
                  ? member.posture().crouch() > 0 || member.posture().fallen() > 0
                  : unit.model().state().pose().proneCause() != megamek.common.units.ProneCause.NONE)) { continue; }
            var frame = new Matrix4(instance.transform).mul(body.root.globalTransform);
            var center = new Vector3(0, 0, body.bounds.min.z).mul(frame);
            float ground = UnitLandingSupports.ground(scene, center.x, center.y, surfaces);
            // A roof, bridge, airborne elevation or unmodelled cliff step is not this unit's ground plane.
            if (!Float.isFinite(ground) || Math.abs(center.z - ground) > 2 * BoardGeometry.HEX_SCALE) { continue; }
            float span = Math.max(2, Math.min(body.bounds.getWidth(), body.bounds.getHeight()) * .3f);
            float dx = difference(scene, frame, new Vector3(span, 0, 0));
            float dy = difference(scene, frame, new Vector3(0, span, 0));
            float up = new Vector3(Vector3.Z).rot(frame).z;
            if (!Float.isFinite(dx + dy) || Math.abs(dx) + Math.abs(dy) < .0001f || up <= .0001f) { continue; }
            var normal = new Vector3(-dx / (2 * span * up), -dy / (2 * span * up), 1).nor();
            // A rigid contact plane is sufficient for rendered road ramps; do not tilt bodies against cliff faces.
            if (normal.z < .7f) { continue; }
            body.root.rotation.mul(new Quaternion().setFromCross(Vector3.Z, normal));
            instance.calculateTransforms();
            float lift = Float.NEGATIVE_INFINITY;
            for (var contact : body.contacts) {
                var point = new Vector3(contact.point).mul(contact.node.globalTransform).mul(instance.transform);
                float surface = UnitLandingSupports.ground(scene, point.x, point.y, surfaces);
                if (Float.isFinite(surface)) { lift = Math.max(lift, surface - point.z); }
            }
            if (Float.isFinite(lift) && Math.abs(lift) <= BoardGeometry.LEVEL) {
                float parentUp = body.root.getParent() == null ? instance.transform.getScaleZ()
                      : new Vector3(Vector3.Z).rot(body.root.getParent().globalTransform).rot(instance.transform).z;
                body.root.translation.z += lift / parentUp;
            }
            instance.calculateTransforms();
            changed = true;
        }
        return changed;
    }

    private float difference(BoardScene scene, Matrix4 frame, Vector3 offset) {
        var plus = new Vector3(offset).mul(frame);
        var minus = new Vector3(offset).scl(-1).mul(frame);
        return UnitLandingSupports.ground(scene, plus.x, plus.y, surfaces) - UnitLandingSupports.ground(scene, minus.x, minus.y, surfaces);
    }
}
