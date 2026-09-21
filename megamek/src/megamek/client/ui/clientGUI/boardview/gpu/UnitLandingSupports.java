/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import megamek.common.board.Coords;

/** Per-instance landing gear. Hull placement is finished before contacts are sampled; no game state is changed. */
final class UnitLandingSupports {
    private final ModelInstance instance;
    private final BoardSurface.Cache surfaces;
    private final List<Support> supports = new ArrayList<>();

    private static final class Support {
        final UnitModelDescriptor.LandingSupport definition;
        final Node parent, shaft, foot;
        final Vector3 restParent, restFoot, restScale;
        final Map<NodePart, Boolean> parts = new IdentityHashMap<>();

        Support(UnitModelDescriptor.LandingSupport definition, Node parent, Node shaft, Node foot,
              Node restParent, Node restFoot, Node restShaft) {
            this.definition = definition;
            this.parent = parent;
            this.shaft = shaft;
            this.foot = foot;
            this.restParent = new Vector3(restParent.translation);
            this.restFoot = new Vector3(restFoot.translation);
            restScale = new Vector3(restShaft.scale);
            remember(parent);
        }

        private void remember(Node node) {
            node.parts.forEach(part -> parts.put(part, part.enabled));
            node.getChildren().forEach(this::remember);
        }

        void visible(boolean visible) {
            parts.forEach((part, enabled) -> part.enabled = visible && enabled);
        }

        void reset() {
            parent.translation.set(restParent);
            foot.translation.set(restFoot);
            shaft.scale.set(restScale);
        }

        void deploy(float deployment, float reach) {
            parent.translation.mulAdd(UnitModelDescriptor.vector(definition.stowedOffset()), 1 - deployment);
            foot.translation.z -= reach * deployment;
            shaft.scale.z = restScale.z * (1 + reach * deployment / definition.length());
            // At zero the complete subtree is inside the opaque hull; omitting it also avoids internal picking/shadows.
            visible(deployment > 0);
        }
    }

    UnitLandingSupports(ModelInstance instance, ModelInstance rest, List<UnitRig> rigs, BoardSurface.Cache surfaces) {
        this.instance = instance;
        this.surfaces = surfaces;
        for (var rig : rigs) {
            Node container = rig.container() == null ? null : instance.getNode(rig.container());
            Node original = rig.container() == null ? null : rest.getNode(rig.container());
            if (rig.container() != null && (container == null || original == null)) {
                continue;
            }
            var nodes = container == null ? instance.nodes : container.getChildren();
            var originals = original == null ? rest.nodes : original.getChildren();
            for (var definition : rig.landingSupports()) {
                supports.add(new Support(definition, UnitAnimator.find(nodes, definition.node()),
                      UnitAnimator.find(nodes, definition.shaft()), UnitAnimator.find(nodes, definition.foot()),
                      UnitAnimator.find(originals, definition.node()), UnitAnimator.find(originals, definition.foot()),
                      UnitAnimator.find(originals, definition.shaft())));
            }
        }
    }

    boolean apply(BoardScene scene, BoardScene.Unit unit, UnitMotion.Sample motion) {
        // Non-Aero/building units never receive this behavior, even if an author reuses an aircraft mesh.
        if (supports.isEmpty() || unit.location().aeroState() == null) {
            return false;
        }
        boolean landed = !unit.sensorContact() && motion.aeroState(unit) == BoardScene.AeroState.LANDED;
        float deployment = !landed ? 0 : motion.gear() == null ? 1 : motion.gear().deployment();
        supports.forEach(Support::reset);
        instance.calculateTransforms();
        for (Support support : supports) {
            if (deployment == 0 || unit.footprint().size() < 2) {
                support.deploy(deployment, 0);
                continue;
            }
            Vector3 contact = UnitModelDescriptor.vector(support.definition.contact())
                  .mul(support.foot.globalTransform).mul(instance.transform);
            float ground = ground(scene, contact.x, contact.y, surfaces);
            Matrix4 frame = new Matrix4(instance.transform).mul(support.parent.globalTransform);
            Vector3 up = new Vector3(Vector3.Z).rot(frame);
            // Landed shafts are vertical. Reject invalid/tilted art or an absent surface rather than stretching sideways.
            if (!Float.isFinite(ground) || !Float.isFinite(up.z) || up.z <= .0001f
                  || Math.abs(up.x) + Math.abs(up.y) > .001f * up.z) {
                support.visible(false);
                continue;
            }
            float reach = Math.max(0, contact.z - ground) / up.z;
            float stretch = support.restScale.z * (1 + reach / support.definition.length());
            if (!Float.isFinite(reach) || !Float.isFinite(stretch)) {
                support.visible(false);
                continue;
            }
            support.deploy(deployment, reach);
        }
        instance.calculateTransforms();
        return true;
    }

    /** Reuse the rendered ground/road/bank triangles. Liquid cannot carry a pad; ice can. Elevation-0 is below roofs. */
    static float ground(BoardScene scene, float x, float y) {
        return ground(scene, x, y, null);
    }

    static float ground(BoardScene scene, float x, float y, BoardSurface.Cache surfaces) {
        if (!Float.isFinite(x) || !Float.isFinite(y)) {
            return Float.NaN;
        }
        int column = (int) Math.floor(x / (BoardGeometry.WIDTH * .75f));
        int row = (int) Math.floor(-y / BoardGeometry.HEIGHT);
        float height = Float.NEGATIVE_INFINITY;
        for (int cx = Math.max(0, column - 1); cx <= Math.min(scene.width() - 1, column + 1); cx++) {
            for (int cy = Math.max(0, row - 1); cy <= Math.min(scene.height() - 1, row + 1); cy++) {
                var tile = scene.tile(new Coords(cx, cy));
                if (tile == null || !BoardGeometry.contains(tile.coords(), x, y)) {
                    continue;
                }
                float sample = tile.frozen() ? BoardGeometry.surfaceZ(tile)
                      : (surfaces == null ? new BoardSurface(scene, tile) : surfaces.get(scene, tile)).height(x, y);
                if (!tile.liquid().present() || tile.frozen() || sample >= BoardGeometry.waterZ(tile)) {
                    height = Math.max(height, sample);
                }
            }
        }
        return Float.isFinite(height) ? height : Float.NaN;
    }
}
