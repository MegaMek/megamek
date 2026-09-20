/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.math.Vector3;

/** Shared rigid-body lifecycle for vehicle, aircraft, naval, ProtoMek and static fallback art. */
final class FamilyVisual {
    private static final float[] SIZE_SCALES = { .78f, .9f, 1, 1.12f, 1.25f };

    private FamilyVisual() { }

    static GpuMeeple assemble(GpuUnitModels library, JsonValue descriptor, UnitModelState.Structure structure) {
        var form = structure.bodyForm();
        var forms = descriptor.get("forms");
        if (form != null && forms != null && forms.has(form.configuration())) {
            // Resolve one authored form; do not recursively follow descriptors or implement conversion rules here.
            descriptor = library.descriptor(forms.getString(form.configuration()));
        }
        var body = library.modular(descriptor.getString("body"));
        if (body == null || !"body".equals(body.descriptor().kind())) {
            throw new IllegalArgumentException("Missing family body");
        }
        Model assembled = new Model();
        try {
            for (Node node : new ModelInstance(body.model()).nodes) {
                assembled.nodes.add(node);
            }
            assembled.calculateTransforms();
            var bindings = UnitEquipmentAssembly.attachAll(library, descriptor, body, structure, assembled);
            float scale = 1;
            if (form != null) {
                // Keep the sockets/joints even when the unit has no turret body.
                if ("vehicle".equals(descriptor.getString("family")) || "naval".equals(descriptor.getString("family"))) {
                    hideBody(assembled.getNode("turret"), form.turrets() < 1);
                    hideBody(assembled.getNode("turret2"), form.turrets() < 2);
                }
                scale = SIZE_SCALES[Math.clamp(form.size() - 1, 0, SIZE_SCALES.length - 1)];
                assembled.getNode(body.descriptor().joints().get("root")).scale.scl(scale);
            }
            assembled.calculateTransforms();
            var bounds = body.descriptor().bounds();
            Vector3 dimensions = UnitModelDescriptor.vector(bounds.max()).sub(UnitModelDescriptor.vector(bounds.min())).scl(scale);
            return new GpuMeeple(assembled, null, true, bindings, 1f / 27, dimensions,
                  java.util.List.of(new UnitRig(body.descriptor())), UnitFamilyScale.forFamily(descriptor.getString("family")));
        } catch (RuntimeException error) {
            assembled.dispose();
            throw error;
        }
    }

    private static void hideBody(Node node, boolean hide) {
        if (node != null && hide) {
            node.parts.forEach(part -> part.enabled = false);
        }
    }
}
